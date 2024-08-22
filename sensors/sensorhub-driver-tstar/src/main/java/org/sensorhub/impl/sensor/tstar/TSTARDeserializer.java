//package org.sensorhub.impl.sensor.tstar;
//
//import com.google.gson.*;
//
//import java.lang.reflect.Type;
//
//public class TSTARDeserializer<T> implements JsonDeserializer<T> {
//    private final Class mNestedClazz;
//    private final Object mNestedDeserializer;
//
//    public TSTARDeserializer(Class nestedClazz, Object nestedDeserializer) {
//        mNestedClazz = nestedClazz;
//        mNestedDeserializer = nestedDeserializer;
//    }
//
//    @Override
//    public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
//        // Get the "content" element from the parsed JSON
//        JsonElement content = je.getAsJsonObject().get("content");
//
//        // Deserialize it. You use a new instance of Gson to avoid infinite recursion
//        // to this deserializer
//        GsonBuilder builder = new GsonBuilder();
//        if (mNestedClazz != null && mNestedDeserializer != null) {
//            builder.registerTypeAdapter(mNestedClazz, mNestedDeserializer);
//        }
//        return builder.create().fromJson(content, type);
//
//    }
//    MyDeserializer<Content> myDeserializer = new MyDeserializer<Content>(SubContent.class,
//            new SubContentDeserializer());
//    Gson gson = new GsonBuilder().registerTypeAdapter(Content.class, myDeserializer).create();
//}
