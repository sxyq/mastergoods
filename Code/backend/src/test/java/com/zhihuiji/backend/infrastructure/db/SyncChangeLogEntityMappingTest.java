package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.zhihuiji.backend.domain.entity.SyncChangeLogEntity;
import jakarta.persistence.Lob;
import java.lang.reflect.Field;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class SyncChangeLogEntityMappingTest {
    @Test
    void payloadUsesTextBindingInsteadOfPostgresLobLocator() throws Exception {
        Field field = SyncChangeLogEntity.class.getDeclaredField("payload");

        assertFalse(field.isAnnotationPresent(Lob.class));
        assertEquals(
            SqlTypes.LONGVARCHAR,
            field.getAnnotation(JdbcTypeCode.class).value()
        );
    }
}
