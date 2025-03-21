package ru.otus.backend.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    // Этот метод вызывается Spring'ом для определения, какой DataSource использовать
    @Override
    protected Object determineCurrentLookupKey() {
        // Проверяем, является ли текущая транзакция READ-ONLY
        // Если да - используем slave, если нет - master
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? "slave"
                : "master";
    }
}