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
 * La busqueda va en dos consultas y no en una: si el texto coincide con un
 * propietario, la fila debe mostrar igualmente al arrendatario y a los deudores,
 * y filtrar y proyectar a la vez devolveria solo las partes coincidentes.
 * Ademas, {@code JOIN FETCH} de una coleccion mas paginacion obliga a Hibernate
 * a paginar en memoria (aviso {@code HHH90003004}).
 */
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /** Sin escapar, teclear {@code %} devolveria todos los contratos. */
    String LIKE_ESCAPE_CHARACTER = "!";

    /**
     * Identificadores de los contratos cuyo texto coincide, paginados.
     *
     * <p>{@code EXISTS} y no {@code JOIN} sobre las partes: el JOIN multiplica
     * filas y obligaria a un {@code DISTINCT}, que en MySQL impide ordenar por
     * una columna ausente de la proyeccion (error 3065) y descuadra el conteo.</p>
     *
     * <p>Sin {@code LOWER()}: mayusculas y tildes las resuelve la colacion
     * {@code utf8mb4_0900_ai_ci}, y {@code LOWER()} no cubre las tildes.</p>
     *
     * @param pattern patron {@code LIKE} con los comodines ya escapados
     * @param pageable debe llegar <em>sin</em> ordenacion: el orden lo fija la
     *                 consulta, y Spring Data concatenaria un segundo ORDER BY
     * @return una pagina de identificadores, vacia si no hay coincidencias
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
     * Carga los contratos indicados con inmueble, partes y personas, en una
     * unica consulta, de modo que recorrer el grafo no dispare mas consultas.
     *
     * <p>{@code LEFT JOIN FETCH} para que un contrato sin partes no desaparezca.
     * Sin {@code DISTINCT}: desde Hibernate 6 una consulta con raiz de entidad
     * ya elimina los duplicados del producto cartesiano.</p>
     *
     * @param ids identificadores a cargar
     * @return los contratos, ordenados por codigo
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
