//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Apr 2, 2014
// ---------------------

package com.cleversafe.og.cli.json.type;

import java.io.IOException;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class CaseInsensitiveEnumTypeAdapterFactory implements TypeAdapterFactory
{
   @Override
   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type)
   {
      @SuppressWarnings("unchecked")
      final Class<T> rawType = (Class<T>) type.getRawType();
      if (!rawType.isEnum() || behavioralEnum(rawType))
         return null;

      return new TypeAdapter<T>()
      {
         @Override
         public void write(final JsonWriter out, final T value) throws IOException
         {
            out.value(value.toString().toLowerCase(Locale.US));
         }

         @SuppressWarnings("unchecked")
         @Override
         public T read(final JsonReader in) throws IOException
         {
            final String s = in.nextString().toUpperCase(Locale.US);
            for (final Object enumEntry : rawType.getEnumConstants())
            {
               if (enumEntry.toString().equals(s))
                  return (T) enumEntry;
            }
            throw new IllegalArgumentException(String.format("Could not parse into enum [%s]", s));
         }
      }.nullSafe();
   }

   private boolean behavioralEnum(final Class<?> rawType)
   {
      final Class<?> parent = rawType.getSuperclass();
      return parent != null && parent.isEnum();
   }
}
