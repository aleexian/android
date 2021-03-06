/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service.vertex;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;
import com.android.tools.idea.editors.gfxtrace.service.path.BlobPath;

import java.io.IOException;

public final class VertexBuffer implements BinaryObject {
  //<<<Start:Java.ClassBody:1>>>
  private VertexStream[] myStreams;

  // Constructs a default-initialized {@link VertexBuffer}.
  public VertexBuffer() {}


  public VertexStream[] getStreams() {
    return myStreams;
  }

  public VertexBuffer setStreams(VertexStream[] v) {
    myStreams = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("vertex", "Buffer", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Streams", new Slice("", new Pointer(new Struct(VertexStream.Klass.INSTANCE.entity())))),
    });
    Namespace.register(Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public Entity entity() { return ENTITY; }

    @Override @NotNull
    public BinaryObject create() { return new VertexBuffer(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      VertexBuffer o = (VertexBuffer)obj;
      e.uint32(o.myStreams.length);
      for (int i = 0; i < o.myStreams.length; i++) {
        e.object(o.myStreams[i]);
      }
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      VertexBuffer o = (VertexBuffer)obj;
      o.myStreams = new VertexStream[d.uint32()];
      for (int i = 0; i <o.myStreams.length; i++) {
        o.myStreams[i] = (VertexStream)d.object();
      }
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
