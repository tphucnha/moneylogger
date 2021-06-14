package dev.tphucnha.moneylogger.service.impl;

import dev.tphucnha.moneylogger.domain.Category;
import dev.tphucnha.moneylogger.domain.Transaction;
import dev.tphucnha.moneylogger.repository.CategoryRepository;
import dev.tphucnha.moneylogger.repository.TransactionRepository;
import dev.tphucnha.moneylogger.security.SecurityUtils;
import dev.tphucnha.moneylogger.service.TransactionService;
import dev.tphucnha.moneylogger.service.dto.TransactionDTO;
import dev.tphucnha.moneylogger.service.mapper.CategoryMapper;
import dev.tphucnha.moneylogger.service.mapper.TransactionMapper;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Transaction}.
 */
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;

    private final CategoryRepository categoryRepository;

    private final TransactionMapper transactionMapper;

    private final CategoryMapper categoryMapper;

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        CategoryRepository categoryRepository,
        TransactionMapper transactionMapper,
        CategoryMapper categoryMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionMapper = transactionMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public TransactionDTO save(TransactionDTO transactionDTO) {
        log.debug("Request to save Transaction : {}", transactionDTO);
        validateDto(transactionDTO);
        Transaction transaction = transactionMapper.toEntity(transactionDTO);

        if (transactionDTO.getCategory() != null) {
            if (transactionDTO.getCategory().getId() != null) {
                // No need to validate the existing category's owner, the transactionDTO is validated already
                Category existingCategory = categoryRepository.getOne(transactionDTO.getCategory().getId());
                transaction.setCategory(existingCategory);
            } else {
                Category newCategory = categoryRepository.save(categoryMapper.toEntity(transactionDTO.getCategory()));
                transaction.setCategory(newCategory);
            }
        }

        transaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(transaction);
    }

    @Override
    public Optional<TransactionDTO> partialUpdate(TransactionDTO transactionDTO) {
        log.debug("Request to partially update Transaction : {}", transactionDTO);
        validateDto(transactionDTO);
        return transactionRepository
            .findById(transactionDTO.getId())
            .map(
                existingTransaction -> {
                    transactionMapper.partialUpdate(existingTransaction, transactionDTO);
                    return existingTransaction;
                }
            )
            .map(transactionRepository::save)
            .map(transactionMapper::toDto);
    }

    private void validateDto(TransactionDTO transactionDTO) {
        // Validate category's owner
        if (transactionDTO.getCategory() != null && transactionDTO.getCategory().getId() != null) {
            Optional<Category> target = categoryRepository.findById(transactionDTO.getCategory().getId());
            if (target.isEmpty()) throw new InvalidDataAccessResourceUsageException("Invalid category");

            if (
                !target.get().getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))
            ) throw new InvalidDataAccessResourceUsageException("Invalid category");
        }

        // Validate transaction's owner
        if (transactionDTO.getId() != null) {
            Optional<Transaction> target = transactionRepository.findById(transactionDTO.getId());
            if (target.isEmpty()) throw new InvalidDataAccessResourceUsageException("Invalid transaction");

            if (!target.get().getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))) throw new AccessDeniedException(
                "Access denied"
            );
        }
    }

    //    @Override
    //    @Transactional(readOnly = true)
    //    public Page<TransactionDTO> findAll(Pageable pageable) {
    //        log.debug("Request to get all Transactions");
    //        return transactionRepository.findAll(pageable).map(transactionMapper::toDto);
    //    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionDTO> findOne(Long id) {
        log.debug("Request to get Transaction : {}", id);
        Optional<Transaction> transaction = transactionRepository.findById(id);
        validateEntity(transaction);
        return transaction.map(transactionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Transaction : {}", id);
        Optional<Transaction> transaction = transactionRepository.findById(id);
        validateEntity(transaction);
        transactionRepository.deleteById(id);
    }

    private void validateEntity(Optional<Transaction> transaction) {
        if (transaction.isPresent()) {
            if (
                !Objects.equals(transaction.get().getCreatedBy(), SecurityUtils.getCurrentUserLogin().orElse(""))
            ) throw new AccessDeniedException("Access denied");
        }
    }
}
