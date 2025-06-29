package ru.otus.counterservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tarantool.TarantoolClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TarantoolUnreadCounterClient {

    private final TarantoolClient client;

    public long countUnreadMessages(Long userId) {

        List<?> result = client.syncOps().eval("return box.func.count_unread:call(.)", List.of(userId));

        if (result != null && !result.isEmpty()) {
            Map<String, Object> resultMap = (Map<String, Object>) result.get(0);
            if (Boolean.TRUE.equals(resultMap.get("success"))) {
                return ((Number) resultMap.get("count")).longValue();
            }
        }
        return ((Number) result.get(0)).longValue();
    }
}

