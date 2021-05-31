package dev.tphucnha.moneylogger.web.rest;

import static dev.tphucnha.moneylogger.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.tphucnha.moneylogger.IntegrationTest;
import dev.tphucnha.moneylogger.domain.Category;
import dev.tphucnha.moneylogger.domain.Transaction;
import dev.tphucnha.moneylogger.repository.CategoryRepository;
import dev.tphucnha.moneylogger.repository.TransactionRepository;
import dev.tphucnha.moneylogger.service.dto.CategoryDTO;
import dev.tphucnha.moneylogger.service.dto.TransactionDTO;
import dev.tphucnha.moneylogger.service.mapper.CategoryMapper;
import dev.tphucnha.moneylogger.service.mapper.TransactionMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link TransactionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class TransactionResourceIT {

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT = new BigDecimal(2);
    private static final BigDecimal SMALLER_AMOUNT = new BigDecimal(1 - 1);

    private static final String DEFAULT_DETAILS = "AAAAAAAAAA";
    private static final String UPDATED_DETAILS = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/transactions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String CATEGORY_ENTITY_API_URL = "/api/categories";
    private static final String CATEGORY_ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTransactionMockMvc;

    private Transaction transaction;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Transaction createEntity(EntityManager em) {
        return new Transaction().amount(DEFAULT_AMOUNT).details(DEFAULT_DETAILS);
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Transaction createUpdatedEntity(EntityManager em) {
        return new Transaction().amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);
    }

    @BeforeEach
    public void initTest() {
        transaction = createEntity(em);
    }

    @Test
    @Transactional
    void createTransaction() throws Exception {
        int databaseSizeBeforeCreate = transactionRepository.findAll().size();
        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);
        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isCreated());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeCreate + 1);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(DEFAULT_DETAILS);
    }

    @Test
    @Transactional
    void createTransactionWithExistingId() throws Exception {
        // Create the Transaction with an existing ID
        transaction.setId(1L);
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        int databaseSizeBeforeCreate = transactionRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = transactionRepository.findAll().size();
        // set the field null
        transaction.setAmount(null);

        // Create the Transaction, which fails.
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDetailsIsRequired() throws Exception {
        int databaseSizeBeforeTest = transactionRepository.findAll().size();
        // set the field null
        transaction.setDetails(null);

        // Create the Transaction, which fails.
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTransactions() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(transaction.getId().intValue())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(DEFAULT_AMOUNT))))
            .andExpect(jsonPath("$.[*].details").value(hasItem(DEFAULT_DETAILS)));
    }

    @Test
    @Transactional
    void getTransaction() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get the transaction
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL_ID, transaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(transaction.getId().intValue()))
            .andExpect(jsonPath("$.amount").value(sameNumber(DEFAULT_AMOUNT)))
            .andExpect(jsonPath("$.details").value(DEFAULT_DETAILS));
    }

    @Test
    @Transactional
    void getTransactionsByIdFiltering() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        Long id = transaction.getId();

        defaultTransactionShouldBeFound("id.equals=" + id);
        defaultTransactionShouldNotBeFound("id.notEquals=" + id);

        defaultTransactionShouldBeFound("id.greaterThanOrEqual=" + id);
        defaultTransactionShouldNotBeFound("id.greaterThan=" + id);

        defaultTransactionShouldBeFound("id.lessThanOrEqual=" + id);
        defaultTransactionShouldNotBeFound("id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount equals to DEFAULT_AMOUNT
        defaultTransactionShouldBeFound("amount.equals=" + DEFAULT_AMOUNT);

        // Get all the transactionList where amount equals to UPDATED_AMOUNT
        defaultTransactionShouldNotBeFound("amount.equals=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsNotEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount not equals to DEFAULT_AMOUNT
        defaultTransactionShouldNotBeFound("amount.notEquals=" + DEFAULT_AMOUNT);

        // Get all the transactionList where amount not equals to UPDATED_AMOUNT
        defaultTransactionShouldBeFound("amount.notEquals=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsInShouldWork() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount in DEFAULT_AMOUNT or UPDATED_AMOUNT
        defaultTransactionShouldBeFound("amount.in=" + DEFAULT_AMOUNT + "," + UPDATED_AMOUNT);

        // Get all the transactionList where amount equals to UPDATED_AMOUNT
        defaultTransactionShouldNotBeFound("amount.in=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount is not null
        defaultTransactionShouldBeFound("amount.specified=true");

        // Get all the transactionList where amount is null
        defaultTransactionShouldNotBeFound("amount.specified=false");
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount is greater than or equal to DEFAULT_AMOUNT
        defaultTransactionShouldBeFound("amount.greaterThanOrEqual=" + DEFAULT_AMOUNT);

        // Get all the transactionList where amount is greater than or equal to UPDATED_AMOUNT
        defaultTransactionShouldNotBeFound("amount.greaterThanOrEqual=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount is less than or equal to DEFAULT_AMOUNT
        defaultTransactionShouldBeFound("amount.lessThanOrEqual=" + DEFAULT_AMOUNT);

        // Get all the transactionList where amount is less than or equal to SMALLER_AMOUNT
        defaultTransactionShouldNotBeFound("amount.lessThanOrEqual=" + SMALLER_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount is less than DEFAULT_AMOUNT
        defaultTransactionShouldNotBeFound("amount.lessThan=" + DEFAULT_AMOUNT);

        // Get all the transactionList where amount is less than UPDATED_AMOUNT
        defaultTransactionShouldBeFound("amount.lessThan=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where amount is greater than DEFAULT_AMOUNT
        defaultTransactionShouldNotBeFound("amount.greaterThan=" + DEFAULT_AMOUNT);

        // Get all the transactionList where amount is greater than SMALLER_AMOUNT
        defaultTransactionShouldBeFound("amount.greaterThan=" + SMALLER_AMOUNT);
    }

    @Test
    @Transactional
    void getAllTransactionsByDetailsIsEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where details equals to DEFAULT_DETAILS
        defaultTransactionShouldBeFound("details.equals=" + DEFAULT_DETAILS);

        // Get all the transactionList where details equals to UPDATED_DETAILS
        defaultTransactionShouldNotBeFound("details.equals=" + UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void getAllTransactionsByDetailsIsNotEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where details not equals to DEFAULT_DETAILS
        defaultTransactionShouldNotBeFound("details.notEquals=" + DEFAULT_DETAILS);

        // Get all the transactionList where details not equals to UPDATED_DETAILS
        defaultTransactionShouldBeFound("details.notEquals=" + UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void getAllTransactionsByDetailsIsInShouldWork() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where details in DEFAULT_DETAILS or UPDATED_DETAILS
        defaultTransactionShouldBeFound("details.in=" + DEFAULT_DETAILS + "," + UPDATED_DETAILS);

        // Get all the transactionList where details equals to UPDATED_DETAILS
        defaultTransactionShouldNotBeFound("details.in=" + UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void getAllTransactionsByDetailsIsNullOrNotNull() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where details is not null
        defaultTransactionShouldBeFound("details.specified=true");

        // Get all the transactionList where details is null
        defaultTransactionShouldNotBeFound("details.specified=false");
    }

    @Test
    @Transactional
    void getAllTransactionsByDetailsContainsSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where details contains DEFAULT_DETAILS
        defaultTransactionShouldBeFound("details.contains=" + DEFAULT_DETAILS);

        // Get all the transactionList where details contains UPDATED_DETAILS
        defaultTransactionShouldNotBeFound("details.contains=" + UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void getAllTransactionsByDetailsNotContainsSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get all the transactionList where details does not contain DEFAULT_DETAILS
        defaultTransactionShouldNotBeFound("details.doesNotContain=" + DEFAULT_DETAILS);

        // Get all the transactionList where details does not contain UPDATED_DETAILS
        defaultTransactionShouldBeFound("details.doesNotContain=" + UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void getAllTransactionsByCategoryIsEqualToSomething() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);
        Category category = CategoryResourceIT.createEntity(em);
        em.persist(category);
        em.flush();
        transaction.setCategory(category);
        transactionRepository.saveAndFlush(transaction);
        Long categoryId = category.getId();

        // Get all the transactionList where category equals to categoryId
        defaultTransactionShouldBeFound("categoryId.equals=" + categoryId);

        // Get all the transactionList where category equals to (categoryId + 1)
        defaultTransactionShouldNotBeFound("categoryId.equals=" + (categoryId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultTransactionShouldBeFound(String filter) throws Exception {
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(transaction.getId().intValue())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(DEFAULT_AMOUNT))))
            .andExpect(jsonPath("$.[*].details").value(hasItem(DEFAULT_DETAILS)));

        // Check, that the count call also returns 1
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultTransactionShouldNotBeFound(String filter) throws Exception {
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingTransaction() throws Exception {
        // Get the transaction
        restTransactionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewTransaction() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction
        Transaction updatedTransaction = transactionRepository.findById(transaction.getId()).get();
        // Disconnect from session so that the updates on updatedTransaction are not directly saved in db
        em.detach(updatedTransaction);
        updatedTransaction.amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);
        TransactionDTO transactionDTO = transactionMapper.toDto(updatedTransaction);

        restTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, transactionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void putNonExistingTransaction() throws Exception {
        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();
        transaction.setId(count.incrementAndGet());

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, transactionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTransaction() throws Exception {
        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();
        transaction.setId(count.incrementAndGet());

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTransaction() throws Exception {
        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();
        transaction.setId(count.incrementAndGet());

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTransactionMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(transactionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTransactionWithPatch() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction using partial update
        Transaction partialUpdatedTransaction = new Transaction();
        partialUpdatedTransaction.setId(transaction.getId());

        partialUpdatedTransaction.details(UPDATED_DETAILS);

        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedTransaction))
            )
            .andExpect(status().isOk());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void fullUpdateTransactionWithPatch() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction using partial update
        Transaction partialUpdatedTransaction = new Transaction();
        partialUpdatedTransaction.setId(transaction.getId());

        partialUpdatedTransaction.amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);

        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedTransaction))
            )
            .andExpect(status().isOk());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(UPDATED_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(UPDATED_DETAILS);
    }

    @Test
    @Transactional
    void patchNonExistingTransaction() throws Exception {
        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();
        transaction.setId(count.incrementAndGet());

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, transactionDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTransaction() throws Exception {
        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();
        transaction.setId(count.incrementAndGet());

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTransaction() throws Exception {
        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();
        transaction.setId(count.incrementAndGet());

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTransaction() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeDelete = transactionRepository.findAll().size();

        // Delete the transaction
        restTransactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, transaction.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    @WithMockUser(username = "the-owner")
    void theTransactionShouldNotToBeFoundByWhoIsNotItsOwner() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get the transaction
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL_ID, transaction.getId()).with(user("not-the-owner")))
            .andExpect(authenticated().withUsername("not-the-owner"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    @Transactional
    void userShouldSeeNothingIfTheyDidNotCreateAnyTransaction() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        // Get the transaction
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL).with(user("not-the-owner")))
            .andExpect(authenticated().withUsername("not-the-owner"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void userShouldSeeOnlyTheirsCategories() throws Exception {
        int databaseSizeBeforeCreate = transactionRepository.findAll().size();

        // Initialize the database: the-owner's transaction
        transactionRepository.saveAndFlush(transaction);

        // the-others-transaction
        TransactionDTO othersTransactionDTO = transactionMapper.toDto(new Transaction().details("CCCCCCCCC").amount(BigDecimal.TEN));
        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(othersTransactionDTO))
                    .with(user("the-others"))
            )
            .andExpect(authenticated().withUsername("the-others"))
            .andExpect(status().isCreated());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeCreate + 2);

        // Get all the categories of `user`
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(authenticated().withUsername("user"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$", hasSize(databaseSizeBeforeCreate + 1)))
            .andExpect(jsonPath("$.[*].id").value(hasItem(transaction.getId().intValue())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(DEFAULT_AMOUNT))))
            .andExpect(jsonPath("$.[*].details").value(hasItem(DEFAULT_DETAILS)));

        // Get all the categories of `the-others`
        restTransactionMockMvc
            .perform(get(ENTITY_API_URL).with(user("the-others")))
            .andExpect(authenticated().withUsername("the-others"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$", hasSize(databaseSizeBeforeCreate + 1)))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(othersTransactionDTO.getAmount()))))
            .andExpect(jsonPath("$.[*].details").value(hasItem(othersTransactionDTO.getDetails())));
    }

    @Test
    @Transactional
    void transactionShouldNotBeEditingByWhoIsNotTheOwner() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction
        Transaction updatedTransaction = transactionRepository.findById(transaction.getId()).get();
        // Disconnect from session so that the updates on updatedTransaction are not directly saved in db
        em.detach(updatedTransaction);
        updatedTransaction.amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);
        TransactionDTO transactionDTO = transactionMapper.toDto(updatedTransaction);

        restTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, transaction.getId())
                    .with(user("not-the-owner"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
            .andExpect(authenticated().withUsername("not-the-owner"))
            .andExpect(status().isForbidden());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(DEFAULT_DETAILS);
    }

    @Test
    @Transactional
    void transactionShouldNotBeDeletingByWhoIsNotTheOwner() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeDelete = transactionRepository.findAll().size();

        // Delete the transaction
        restTransactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, transaction.getId()).accept(MediaType.APPLICATION_JSON).with(user("not-the-owner")))
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
            .andExpect(authenticated().withUsername("not-the-owner"))
            .andExpect(status().isForbidden());

        // Validate the database contains one less item
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeDelete);
    }

    @Test
    @Transactional
    void patchUpdateIsNotPermittedIfUserIsNotTheOwner() throws Exception {
        // Initialize the database
        transactionRepository.saveAndFlush(transaction);

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction using partial update
        Transaction partialUpdatedTransaction = new Transaction();
        partialUpdatedTransaction.setId(transaction.getId());

        partialUpdatedTransaction.amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);

        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTransaction.getId())
                    .with(user("not-the-owner"))
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedTransaction))
            )
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
            .andExpect(authenticated().withUsername("not-the-owner"))
            .andExpect(status().isForbidden());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(DEFAULT_DETAILS);
    }

    @Test
    @Transactional
    void userCannotAddTransactionToTheCategoryOfOtherUsers() throws Exception {
        // Save `user's` category to database
        Category userCategory = CategoryResourceIT.createEntity(em);
        categoryRepository.saveAndFlush(userCategory);

        // Disconnect from session so that the updates on updatedCategory are not directly saved in db
        em.detach(userCategory);
        CategoryDTO categoryDTO = categoryMapper.toDto(userCategory);

        int databaseSizeBeforeCreate = transactionRepository.findAll().size();
        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);
        transactionDTO.setCategory(categoryDTO);

        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
                    .with(user("the-other-user"))
            )
            .andExpect(authenticated().withUsername("the-other-user"))
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void userCannotUpdateTransactionToTheCategoryOfOtherUsers() throws Exception {
        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(CategoryResourceIT.createEntity(em));
        restTransactionMockMvc
            .perform(
                post(CATEGORY_ENTITY_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(categoryDTO))
                    .with(user("the-other-user"))
            )
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value(categoryDTO.getName()));

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll();
        Category theOtherUserCategory = categoryList.get(categoryList.size() - 1);
        assertThat(theOtherUserCategory.getName()).isEqualTo(categoryDTO.getName());
        assertThat(theOtherUserCategory.getCreatedBy()).isEqualTo("the-other-user");
        em.detach(theOtherUserCategory);
        CategoryDTO theOtherUserCategoryDto = categoryMapper.toDto(theOtherUserCategory);

        // Initialize the database
        transactionRepository.saveAndFlush(transaction);
        // Validate category before update
        assertThat(transaction.getCategory()).isNull();

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction
        Transaction updatedTransaction = transactionRepository.findById(transaction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTransaction are not directly saved in db
        em.detach(updatedTransaction);
        updatedTransaction.amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);
        TransactionDTO transactionDTO = transactionMapper.toDto(updatedTransaction);
        transactionDTO.setCategory(theOtherUserCategoryDto);

        restTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, transactionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(authenticated().withUsername("user"))
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(DEFAULT_DETAILS);
        assertThat(transaction.getCategory()).isNull();
    }

    @Test
    @Transactional
    void patchUpdateIsNotPermittedIfUserDoesNotOwnTheCategory() throws Exception {
        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(CategoryResourceIT.createEntity(em));
        restTransactionMockMvc
            .perform(
                post(CATEGORY_ENTITY_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(categoryDTO))
                    .with(user("the-other-user"))
            )
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value(categoryDTO.getName()));

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll();
        Category theOtherUserCategory = categoryList.get(categoryList.size() - 1);
        assertThat(theOtherUserCategory.getName()).isEqualTo(categoryDTO.getName());
        assertThat(theOtherUserCategory.getCreatedBy()).isEqualTo("the-other-user");
        em.detach(theOtherUserCategory);
        CategoryDTO theOtherUserCategoryDto = categoryMapper.toDto(theOtherUserCategory);

        // Initialize the database
        transactionRepository.saveAndFlush(transaction);
        // Validate category before update
        assertThat(transaction.getCategory()).isNull();

        int databaseSizeBeforeUpdate = transactionRepository.findAll().size();

        // Update the transaction
        Transaction partialUpdatedTransaction = transactionRepository.findById(transaction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTransaction are not directly saved in db
        em.detach(partialUpdatedTransaction);
        partialUpdatedTransaction.amount(UPDATED_AMOUNT).details(UPDATED_DETAILS);
        TransactionDTO transactionDTO = transactionMapper.toDto(partialUpdatedTransaction);
        transactionDTO.setCategory(theOtherUserCategoryDto);

        restTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
            .andExpect(authenticated().withUsername("user"))
            .andExpect(status().isBadRequest());

        // Validate the Transaction in the database
        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(databaseSizeBeforeUpdate);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(DEFAULT_DETAILS);
        assertThat(transaction.getCategory()).isNull();
    }

    @Test
    @Transactional
    void createTransactionWithCreatingNewCategory() throws Exception {
        int categoryDbSizeBeforeCreate = categoryRepository.findAll().size();
        int transactionDbSizeBeforeCreate = transactionRepository.findAll().size();

        CategoryDTO categoryDTO = categoryMapper.toDto(CategoryResourceIT.createEntity(em));

        // Create the Transaction
        TransactionDTO transactionDTO = transactionMapper.toDto(transaction);
        transactionDTO.setCategory(categoryDTO);
        restTransactionMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(transactionDTO))
            )
            .andExpect(status().isCreated());

        // Validate the Transaction in the database
        List<Category> categoryList = categoryRepository.findAll();
        assertThat(categoryList).hasSize(transactionDbSizeBeforeCreate + 1);
        Category testCategory = categoryList.get(categoryList.size() - 1);
        assertThat(testCategory.getName()).isEqualTo(categoryDTO.getName());

        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(transactionDbSizeBeforeCreate + 1);
        Transaction testTransaction = transactionList.get(transactionList.size() - 1);
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testTransaction.getDetails()).isEqualTo(DEFAULT_DETAILS);
    }

    @Test
    @Transactional
    void deleteCategorizedTransactionWillNotDeleteTheCategory() throws Exception {
        int categoryDbSizeBeforeCreate = categoryRepository.findAll().size();
        int transactionDbSizeBeforeCreate = transactionRepository.findAll().size();

        // Create the Transaction
        Category category = CategoryResourceIT.createEntity(em);
        transaction.setCategory(category);
        transactionRepository.saveAndFlush(transaction);
        em.detach(transaction);

        List<Transaction> transactionList = transactionRepository.findAll();
        assertThat(transactionList.size()).isEqualTo(transactionDbSizeBeforeCreate + 1);
        List<Category> categoryList = categoryRepository.findAll();
        assertThat(categoryList).hasSize(categoryDbSizeBeforeCreate + 1);

        // Delete the transaction
        restTransactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, transaction.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database
        transactionList = transactionRepository.findAll();
        assertThat(transactionList).hasSize(transactionDbSizeBeforeCreate);
        assertThat(categoryList).hasSize(categoryDbSizeBeforeCreate + 1);

    }

    @Test
    @Transactional
    void deteleACategoryWillNotDeleteItsTransaction() throws Exception {
        //TODO
    }

    @Test
    @Transactional
    void updateTransactionByChangingItsCategory() {
        //TODO
    }
}
