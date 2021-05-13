package dev.tphucnha.moneylogger.service.mapper;

import dev.tphucnha.moneylogger.domain.*;
import dev.tphucnha.moneylogger.service.dto.TransactionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Transaction} and its DTO {@link TransactionDTO}.
 */
@Mapper(componentModel = "spring", uses = { CategoryMapper.class })
public interface TransactionMapper extends EntityMapper<TransactionDTO, Transaction> {
    @Mapping(target = "category", source = "category", qualifiedByName = "id")
    TransactionDTO toDto(Transaction s);
}
