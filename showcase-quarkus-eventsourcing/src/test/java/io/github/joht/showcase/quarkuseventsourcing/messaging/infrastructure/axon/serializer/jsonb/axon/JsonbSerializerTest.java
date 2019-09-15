package io.github.joht.showcase.quarkuseventsourcing.messaging.infrastructure.axon.serializer.jsonb.axon;

import static org.mockito.ArgumentMatchers.isA;

import java.beans.ConstructorProperties;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;

import org.axonframework.serialization.AnnotationRevisionResolver;
import org.axonframework.serialization.ChainingConverter;
import org.axonframework.serialization.ContentTypeConverter;
import org.axonframework.serialization.RevisionResolver;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.UnknownSerializedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.joht.showcase.quarkuseventsourcing.messaging.infrastructure.axon.serializer.jsonb.axon.JsonbSerializer;

class JsonbSerializerTest {

    private JsonbSerializer serializerToTest;
    private Instant time;

    @BeforeEach
    void setUp() {
        serializerToTest = JsonbSerializer.defaultSerializer().build();
        time = Instant.now();
    }

    @Test
    void testCanSerializeToStringByteArrayAndInputStream() {
        assertTrue(serializerToTest.canSerializeTo(byte[].class));
        assertTrue(serializerToTest.canSerializeTo(String.class));
        assertTrue(serializerToTest.canSerializeTo(InputStream.class));
    }

    @Test
    void testSerializeAndDeserializeObject_StringFormat() {
        SimpleSerializableType toSerialize = new SimpleSerializableType("first", time,
                new SimpleNestedSerializableType("nested"));

        SerializedObject<String> serialized = serializerToTest.serialize(toSerialize, String.class);

        SimpleSerializableType actual = serializerToTest.deserialize(serialized);
        assertEquals(toSerialize.getValue(), actual.getValue());
        assertEquals(toSerialize.getNested().getValue(), actual.getNested().getValue());
    }

    @Test
    void testSerializeAndDeserializeArray() {
        SimpleSerializableType toSerialize = new SimpleSerializableType("first", time,
                new SimpleNestedSerializableType("nested"));

        SerializedObject<String> serialized = serializerToTest.serialize(new SimpleSerializableType[] { toSerialize }, String.class);

        SimpleSerializableType[] actual = serializerToTest.deserialize(serialized);
        assertEquals(1, actual.length);
        assertEquals(toSerialize.getValue(), actual[0].getValue());
        assertEquals(toSerialize.getNested().getValue(), actual[0].getNested().getValue());
    }

    @Test
    void testSerializeAndDeserializeObject_ByteArrayFormat() {
        SimpleSerializableType toSerialize = new SimpleSerializableType("first", time,
                new SimpleNestedSerializableType("nested"));

        SerializedObject<byte[]> serialized = serializerToTest.serialize(toSerialize, byte[].class);

        SimpleSerializableType actual = serializerToTest.deserialize(serialized);

        assertEquals(toSerialize.getValue(), actual.getValue());
        assertEquals(toSerialize.getNested().getValue(), actual.getNested().getValue());
    }

    @Test
    void testSerializeAndDeserializeObjectUnknownType() {
        SimpleSerializableType toSerialize = new SimpleSerializableType("first", time,
                new SimpleNestedSerializableType("nested"));

        SerializedObject<byte[]> serialized = serializerToTest.serialize(toSerialize, byte[].class);

        Object actual = serializerToTest.deserialize(new SimpleSerializedObject<>(serialized.getData(),
                byte[].class,
                "someUnknownType",
                "42.1"));

        assertTrue(actual instanceof UnknownSerializedType);
    }

    @Test
    void testCustomRevisionResolverAndConverter() {
        RevisionResolver revisionResolver = spy(new AnnotationRevisionResolver());
        ChainingConverter converter = spy(new ChainingConverter());

        serializerToTest = JsonbSerializer.builder()
                .revisionResolver(revisionResolver)
                .converter(converter)
                .build();

        SerializedObject<byte[]> serialized = serializerToTest.serialize(new SimpleNestedSerializableType("test"), byte[].class);
        SimpleNestedSerializableType actual = serializerToTest.deserialize(serialized);

        assertNotNull(actual);
        verify(revisionResolver).revisionOf(SimpleNestedSerializableType.class);
        verify(converter, times(2)).registerConverter(isA(ContentTypeConverter.class));
    }

    @Test
    void testDeserializeNullValue() {
        SerializedObject<byte[]> serializedNull = serializerToTest.serialize(null, byte[].class);
        SimpleSerializedObject<byte[]> serializedNullString = new SimpleSerializedObject<>(
                serializedNull.getData(), byte[].class, serializerToTest.typeForClass(String.class));
        assertNull(serializerToTest.deserialize(serializedNull));
        assertNull(serializerToTest.deserialize(serializedNullString));
    }

    @Test
    void testDeserializeEmptyBytes() {
        assertEquals(Void.class, serializerToTest.classForType(SerializedType.emptyType()));
        assertNull(serializerToTest.deserialize(new SimpleSerializedObject<>(new byte[0], byte[].class, SerializedType.emptyType())));
    }

    public static class ComplexObject {

        private final String value1;
        private final String value2;
        private final int value3;

        @ConstructorProperties({ "value1", "value2", "value3" })
        public ComplexObject(String value1, String value2, int value3) {
            this.value1 = value1;
            this.value2 = value2;
            this.value3 = value3;
        }

        public String getValue1() {
            return value1;
        }

        public String getValue2() {
            return value2;
        }

        public int getValue3() {
            return value3;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComplexObject that = (ComplexObject) o;
            return value3 == that.value3 &&
                    Objects.equals(value1, that.value1) &&
                    Objects.equals(value2, that.value2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value1, value2, value3);
        }
    }

    public static class SimpleSerializableType {

        private final String value;
        private final Instant time;
        private final SimpleNestedSerializableType nested;

        @ConstructorProperties({ "value", "time", "nested" })
        public SimpleSerializableType(String value, Instant time, SimpleNestedSerializableType nested) {
            this.value = value;
            this.time = time;
            this.nested = nested;
        }

        public SimpleNestedSerializableType getNested() {
            return nested;
        }

        public String getValue() {
            return value;
        }

        public Instant getTime() {
            return time;
        }
    }

    public static class SimpleNestedSerializableType {

        private final String value;

        @ConstructorProperties({ "value" })
        public SimpleNestedSerializableType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
