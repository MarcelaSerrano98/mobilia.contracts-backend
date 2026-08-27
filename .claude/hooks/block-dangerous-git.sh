#!/usr/bin/env bash
#
# Guardarrail de git para Claude Code
# ============================================================================
#
# Hook PreToolUse: Claude Code lo ejecuta ANTES de cada comando Bash del
# agente y le pasa por stdin un JSON con la forma:
#
#     {"tool_input": {"command": "git status"}}
#
# Codigo de salida 0 -> el comando se ejecuta.
# Codigo de salida 2 -> el comando se bloquea y nunca llega a ejecutarse.
#
# ----------------------------------------------------------------------------
# QUE SE BLOQUEA Y POR QUE
#
# Solo los comandos que destruyen trabajo de forma irrecuperable, es decir,
# aquellos cuyo resultado git no puede deshacer porque nunca llego a
# registrarlo:
#
#   git reset --hard      descarta los cambios sin commitear
#   git clean -f / -fd    borra archivos sin trackear; git no tiene copia
#   git checkout .        descarta los cambios sin commitear
#   git restore .         idem (salvo --staged, que solo quita del indice)
#   git push --force      puede sobrescribir historia remota de otras personas
#   git branch -D         borra una rama sin comprobar si esta fusionada
#
# NO se bloquea `git push` a secas: anade commits al remoto y no destruye
# nada. Bloquearlo seria control, no seguridad.
#
# ----------------------------------------------------------------------------
# POR QUE ESTE SCRIPT MIRA MAS QUE UN grep
#
# Un `grep "git push"` sobre el comando completo tambien encuentra la cadena
# cuando el comando se limita a MENCIONARLA, y bloquea cosas inofensivas:
#
#   git commit -m "docs: explica git push"     <- solo es texto
#   grep -rn "git reset --hard" README.md      <- solo es una busqueda
#
# Para evitarlo, el script parte el comando en segmentos, se queda con los que
# de verdad invocan a git, y decide mirando el SUBCOMANDO y sus opciones.
#
# Ante la duda, bloquea. Un falso positivo cuesta una molestia; un falso
# negativo cuesta trabajo perdido.
# ============================================================================

set -uo pipefail

ENTRADA=$(cat)

bloquear() {
    echo "BLOQUEADO: $1" >&2
    echo "Motivo: $2" >&2
    echo "Si de verdad hace falta, debe ejecutarlo la persona usuaria a mano." >&2
    exit 2
}

# Sin jq no se puede analizar nada. Se bloquea en lugar de permitir: un
# guardarrail que desaparece en silencio es peor que no tener ninguno.
if ! command -v jq >/dev/null 2>&1; then
    bloquear "(comando no analizable)" "jq no esta instalado y el guardarrail no puede verificar el comando"
fi

COMANDO=$(printf '%s' "$ENTRADA" | jq -r '.tool_input.command // empty' 2>/dev/null)

# No es una llamada a Bash, o no trae comando: nada que revisar.
[[ -z "$COMANDO" ]] && exit 0

# Separa los comandos encadenados con ; | || && para analizarlos uno a uno.
SEGMENTOS=$(printf '%s\n' "$COMANDO" | tr ';|&' '\n\n\n')

while IFS= read -r segmento; do

    # Quita espacios iniciales, un posible 'sudo' y las asignaciones de
    # variables de entorno que preceden al comando (VAR=valor git ...).
    limpio=$(printf '%s' "$segmento" \
        | sed -E 's/^[[:space:]]+//
                  s/^sudo[[:space:]]+//
                  s/^([A-Za-z_][A-Za-z0-9_]*=[^[:space:]]*[[:space:]]+)*//')

    # Solo interesan los segmentos que INVOCAN git, no los que lo nombran.
    [[ "$limpio" =~ ^(/[^[:space:]]*/)?git([[:space:]]|$) ]] || continue

    resto=$(printf '%s' "$limpio" | sed -E 's|^(/[^[:space:]]*/)?git[[:space:]]*||')

    # Descarta las opciones globales previas al subcomando: git -C ruta status
    while true; do
        case "$resto" in
            -C\ *)  resto=$(printf '%s' "$resto" | sed -E 's/^-C[[:space:]]+[^[:space:]]+[[:space:]]*//') ;;
            -c\ *)  resto=$(printf '%s' "$resto" | sed -E 's/^-c[[:space:]]+[^[:space:]]+[[:space:]]*//') ;;
            --no-pager\ *) resto=$(printf '%s' "$resto" | sed -E 's/^--no-pager[[:space:]]*//') ;;
            *) break ;;
        esac
    done

    subcomando=$(printf '%s' "$resto" | awk '{print $1}')
    # Se rodea de espacios para poder buscar opciones completas sin falsos
    # positivos: " --hard " nunca coincide con "--hardware".
    opciones=" $(printf '%s' "$resto" | cut -s -d' ' -f2-) "

    case "$subcomando" in

        reset)
            if [[ "$opciones" == *" --hard "* ]]; then
                bloquear "git $resto" "'reset --hard' descarta todos los cambios sin commitear"
            fi
            ;;

        clean)
            # Cualquier forma de forzado: -f, -fd, -df, -xdf, --force
            if [[ "$opciones" =~ [[:space:]]-[a-zA-Z]*f[a-zA-Z]*[[:space:]] ]] \
               || [[ "$opciones" == *" --force "* ]]; then
                bloquear "git $resto" "'clean -f' borra archivos sin trackear y git no guarda copia de ellos"
            fi
            ;;

        checkout|restore)
            # '--staged' a secas solo quita del indice: no destruye el archivo.
            if [[ "$opciones" == *" --staged "* ]] && [[ "$opciones" != *" --worktree "* ]]; then
                continue
            fi
            if [[ "$opciones" =~ [[:space:]](\.|--[[:space:]]+\.)[[:space:]] ]]; then
                bloquear "git $resto" "'$subcomando .' descarta los cambios sin commitear de todo el arbol"
            fi
            ;;

        push)
            if [[ "$opciones" == *" --force "* ]] \
               || [[ "$opciones" == *" --force-with-lease "* ]] \
               || [[ "$opciones" =~ [[:space:]]-[a-zA-Z]*f[a-zA-Z]*[[:space:]] ]]; then
                bloquear "git $resto" "un push forzado puede sobrescribir historia remota de otras personas"
            fi
            ;;

        branch)
            # -D en mayuscula fuerza el borrado sin comprobar la fusion.
            if [[ "$opciones" =~ [[:space:]]-[a-zA-Z]*D[a-zA-Z]*[[:space:]] ]]; then
                bloquear "git $resto" "'branch -D' borra la rama aunque tenga commits sin fusionar"
            fi
            ;;

    esac

done <<< "$SEGMENTOS"

exit 0
