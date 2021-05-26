package dev.tphucnha.moneylogger.service.impl;

import dev.tphucnha.moneylogger.domain.Category;
import dev.tphucnha.moneylogger.domain.Transaction;
import dev.tphucnha.moneylogger.repository.CategoryRepository;
import dev.tphucnha.moneylogger.repository.TransactionRepository;
import dev.tphucnha.moneylogger.security.SecurityUtils;
import dev.tphucnha.moneylogger.service.TransactionService;
import dev.tphucnha.moneylogger.service.dto.TransactionDTO;
import dev.tphucnha.moneylogger.service.mapper.TransactionMapper;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
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

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        CategoryRepository categoryRepository,
        TransactionMapper transactionMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public TransactionDTO save(TransactionDTO transactionDTO) {
        log.debug("Request to save Transaction : {}", transactionDTO);
        Transaction transaction = transactionMapper.toEntity(transactionDTO);

        // Validate category's owner if exists.
        validateCategoryCreator(transactionDTO);

        if (transaction.getId() != null) {
            Transaction target = transactionRepository.findById(transactionDTO.getId()).orElseThrow(EntityNotFoundException::new);
            if (!target.getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))) throw new AccessDeniedException(
                "Access denied"
            ); else return transactionMapper.toDto(transactionRepository.save(transaction));
        }
        transaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(transaction);
    }

    @Override
    public Optional<TransactionDTO> partialUpdate(TransactionDTO transactionDTO) {
        log.debug("Request to partially update Transaction : {}", transactionDTO);

        // Validate category's owner if exists.
        validateCategoryCreator(transactionDTO);

        Optional<Transaction> target = transactionRepository.findById(transactionDTO.getId());
        if (
            !target.orElseThrow(EntityNotFoundException::new).getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))
        ) throw new AccessDeniedException("Access denied");

        return target
            .map(
                existingTransaction -> {
                    transactionMapper.partialUpdate(existingTransaction, transactionDTO);
                    return existingTransaction;
                }
            )
            .map(transactionRepository::save)
            .map(transactionMapper::toDto);
    }

    private void validateCategoryCreator(TransactionDTO transactionDTO) {
        if (transactionDTO.getCategory() != null && transactionDTO.getCategory().getId() != null) {
            Optional<Category> target = categoryRepository.findById(transactionDTO.getCategory().getId());
            if (
                !target.orElseThrow(EntityNotFoundException::new).getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))
            ) throw new InvalidDataAccessResourceUsageException("Invalid category");
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
        Optional<Transaction> target = transactionRepository.findById(id);
        if (
            target.isPresent() && !target.get().getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))
        ) throw new AccessDeniedException("Access denied");

        return transactionRepository.findById(id).map(transactionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Transaction : {}", id);
        Transaction target = transactionRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        if (!target.getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse(""))) throw new AccessDeniedException("Access denied");

        transactionRepository.deleteById(id);
    }
}
