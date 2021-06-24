package dev.tphucnha.moneylogger.service;

import dev.tphucnha.moneylogger.service.dto.TotalAmountDTO;
import dev.tphucnha.moneylogger.service.dto.TransactionDTO;

import java.util.Optional;

/**
 * Service Interface for managing {@link dev.tphucnha.moneylogger.domain.Transaction}.
 */
public interface TransactionService {
    /**
     * Save a transaction.
     *
     * @param transactionDTO the entity to save.
     * @return the persisted entity.
     */
    TransactionDTO save(TransactionDTO transactionDTO);

    /**
     * Partially updates a transaction.
     *
     * @param transactionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<TransactionDTO> partialUpdate(TransactionDTO transactionDTO);

    /**
     * Get all the transactions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    //    Page<TransactionDTO> findAll(Pageable pageable);

    /**
     * Get the "id" transaction.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TransactionDTO> findOne(Long id);

    /**
     * Delete the "id" transaction.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Get sum of amount of all user's transactions
     * @return total amount of user's transactions
     */
    TotalAmountDTO getTotalAmount();
}
