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
package com.android.tools.idea.editors.gfxtrace.service.path;

import com.android.tools.idea.editors.gfxtrace.service.ContextID;
import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;

import java.io.IOException;

public final class ContextsPath extends Path {
  @Override
  public String getSegmentString() {
    return "Contexts";
  }

  @Override
  public Path getParent() {
    return myCapture;
  }

  public ContextPath context(ContextID id) {
    return new ContextPath().setID(id).setContexts(this);
  }

  //<<<Start:Java.ClassBody:1>>>
  private CapturePath myCapture;

  // Constructs a default-initialized {@link ContextsPath}.
  public ContextsPath() {}


  public CapturePath getCapture() {
    return myCapture;
  }

  public ContextsPath setCapture(CapturePath v) {
    myCapture = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("path", "Contexts", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Capture", new Pointer(new Struct(CapturePath.Klass.INSTANCE.entity()))),
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
    public BinaryObject create() { return new ContextsPath(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      ContextsPath o = (ContextsPath)obj;
      e.object(o.myCapture);
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      ContextsPath o = (ContextsPath)obj;
      o.myCapture = (CapturePath)d.object();
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
