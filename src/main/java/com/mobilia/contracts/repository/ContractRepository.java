package com.mobilia.contracts.repository;

import com.mobilia.contracts.domain.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a datos de {@link Contract}.
 *
 * <p>La busqueda se resuelve en <strong>dos consultas</strong> y no en una sola.
 * El motivo es una sutileza facil de pasar por alto: si se busca "Gomez" y esa
 * persona es <em>propietaria</em>, el contrato debe aparecer en los resultados,
 * pero la tabla tiene que mostrar tambien al arrendatario y a los deudores
 * solidarios, que no contienen el texto buscado. Filtrar y proyectar en la
 * misma consulta devolveria unicamente las partes coincidentes.</p>
 *
 * <p>Ademas, combinar {@code JOIN FETCH} de una coleccion con paginacion obliga
 * a Hibernate a traer todas las filas y paginar en memoria (aviso
 * {@code HHH90003004}). Separar las consultas evita las dos trampas:</p>
 * <ol>
 *   <li>{@link #findMatchingIds} filtra y pagina sobre identificadores.</li>
 *   <li>{@link #findAllWithPartiesByIdIn} carga esos contratos completos en una
 *       unica consulta, evitando el problema N+1.</li>
 * </ol>
 */
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * Caracter de escape de los comodines dentro del patron {@code LIKE}.
     *
     * <p>Sin escapar, un usuario que teclee {@code %} obtendria todos los
     * contratos, y {@code _} actuaria como comodin de un caracter.</p>
     */
    String LIKE_ESCAPE_CHARACTER = "!";

    /**
     * Devuelve, paginados, los identificadores de los contratos en los que el
     * patron aparece en alguno de los campos indicados por el enunciado.
     *
     * <p>Se emplea {@code EXISTS} en lugar de un {@code JOIN} sobre las partes
     * para no multiplicar filas: un contrato con tres partes coincidentes
     * generaria tres filas y obligaria a un {@code DISTINCT}, que a su vez
     * impide ordenar por una columna ausente de la proyeccion (MySQL,
     * error 3065). Con {@code EXISTS} cada contrato aparece una sola vez y la
     * paginacion cuenta lo que debe contar.</p>
     *
     * <p>No se aplica {@code LOWER()} a los operandos: la insensibilidad a
     * mayusculas y a tildes la aporta la colacion {@code utf8mb4_0900_ai_ci}
     * declarada en la migracion. {@code LOWER()} daria una falsa sensacion de
     * portabilidad, porque no resuelve las tildes en ningun motor.</p>
     *
     * @param pattern patron ya construido con comodines y comodines escapados
     * @param pageable pagina solicitada; debe ir <em>sin</em> ordenacion,
     *                 porque el orden lo fija la propia consulta
     */
    @Query(
            value = """
                    SELECT c.id
                    FROM Contract c
                        JOIN c.property p
                    WHERE c.code LIKE :pattern ESCAPE '!'
                       OR p.address LIKE :pattern ESCAPE '!'
                       OR EXISTS (
                            SELECT 1
                            FROM ContractParty cp
                                JOIN cp.person pe
                            WHERE cp.contract = c
                              AND (   pe.firstName LIKE :pattern ESCAPE '!'
                                   OR pe.lastName LIKE :pattern ESCAPE '!'
                                   OR pe.documentNumber LIKE :pattern ESCAPE '!'
                                   OR pe.email LIKE :pattern ESCAPE '!'
                                   OR CONCAT(pe.firstName, ' ', pe.lastName) LIKE :pattern ESCAPE '!')
                       )
                    ORDER BY c.code
                    """,
            countQuery = """
                    SELECT COUNT(c.id)
                    FROM Contract c
                        JOIN c.property p
                    WHERE c.code LIKE :pattern ESCAPE '!'
                       OR p.address LIKE :pattern ESCAPE '!'
                       OR EXISTS (
                            SELECT 1
                            FROM ContractParty cp
                                JOIN cp.person pe
                            WHERE cp.contract = c
                              AND (   pe.firstName LIKE :pattern ESCAPE '!'
                                   OR pe.lastName LIKE :pattern ESCAPE '!'
                                   OR pe.documentNumber LIKE :pattern ESCAPE '!'
                                   OR pe.email LIKE :pattern ESCAPE '!'
                                   OR CONCAT(pe.firstName, ' ', pe.lastName) LIKE :pattern ESCAPE '!')
                       )
                    """
    )
    Page<Long> findMatchingIds(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Carga por completo los contratos indicados: inmueble, partes y persona de
     * cada parte, en una sola consulta.
     *
     * <p>{@code LEFT JOIN FETCH} sobre las partes, y no {@code JOIN FETCH}, para
     * que un contrato sin partes no desapareciera del resultado.</p>
     *
     * <p>No hace falta {@code DISTINCT}: desde Hibernate 6 una consulta cuyo
     * elemento raiz es una entidad elimina por si misma los duplicados que
     * introduce el producto cartesiano del {@code JOIN FETCH}.</p>
     */
    @Query("""
            SELECT c
            FROM Contract c
                JOIN FETCH c.property
                LEFT JOIN FETCH c.parties party
                LEFT JOIN FETCH party.person
            WHERE c.id IN :ids
            ORDER BY c.code
            """)
    List<Contract> findAllWithPartiesByIdIn(@Param("ids") Collection<Long> ids);
}
