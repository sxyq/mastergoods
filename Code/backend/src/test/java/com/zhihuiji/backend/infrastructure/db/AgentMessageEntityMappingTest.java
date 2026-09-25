package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import java.lang.reflect.Field;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class AgentMessageEntityMappingTest {
    @Test
    void contentUsesTextBindingInsteadOfPostgresVarcharLimit() throws Exception {
        Field field = AgentMessageEntity.class.getDeclaredField("content");

        assertFalse(field.isAnnotationPresent(Lob.class));
        assertEquals(
            SqlTypes.LONGVARCHAR,
            field.getAnnotation(JdbcTypeCode.class).value()
        );
        assertEquals("TEXT", field.getAnnotation(Column.class).columnDefinition());
    }

    @Test
    void structuredDataUsesTextBindingInsteadOfPostgresLobLocator() throws Exception {
        Field field = AgentMessageEntity.class.getDeclaredField("structuredDataJson");

        assertFalse(field.isAnnotationPresent(Lob.class));
        assertEquals(
            SqlTypes.LONGVARCHAR,
            field.getAnnotation(JdbcTypeCode.class).value()
        );
    }
}
