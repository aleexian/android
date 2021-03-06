// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: gapid/vertex/vertex.proto

package com.android.tools.idea.editors.gfxtrace.service.vertex;

public final class VertexProtos {
  private VertexProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  /**
   * Protobuf enum {@code vertex.SemanticType}
   *
   * <pre>
   * SemanticType represents the data type of a vertex stream's data.
   * </pre>
   */
  public enum SemanticType
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>Unknown = 0;</code>
     *
     * <pre>
     * Unknown is used when the intended use of a vertex stream is unknown.
     * </pre>
     */
    Unknown(0, 0),
    /**
     * <code>Position = 1;</code>
     *
     * <pre>
     * Position represents a vertex position stream.
     * </pre>
     */
    Position(1, 1),
    /**
     * <code>Color = 2;</code>
     *
     * <pre>
     * Color represents a vertex color stream.
     * </pre>
     */
    Color(2, 2),
    /**
     * <code>Texcoord = 3;</code>
     *
     * <pre>
     * Texcoord represents a texture coordinate stream.
     * </pre>
     */
    Texcoord(3, 3),
    /**
     * <code>Normal = 4;</code>
     *
     * <pre>
     * Normal represents a vertex normal stream.
     * </pre>
     */
    Normal(4, 4),
    /**
     * <code>Tangent = 5;</code>
     *
     * <pre>
     * Tangent represents a vertex tangent stream.
     * </pre>
     */
    Tangent(5, 5),
    /**
     * <code>Bitangent = 6;</code>
     *
     * <pre>
     * Bitangent represents a vertex bitangent stream.
     * </pre>
     */
    Bitangent(6, 6),
    UNRECOGNIZED(-1, -1),
    ;

    /**
     * <code>Unknown = 0;</code>
     *
     * <pre>
     * Unknown is used when the intended use of a vertex stream is unknown.
     * </pre>
     */
    public static final int Unknown_VALUE = 0;
    /**
     * <code>Position = 1;</code>
     *
     * <pre>
     * Position represents a vertex position stream.
     * </pre>
     */
    public static final int Position_VALUE = 1;
    /**
     * <code>Color = 2;</code>
     *
     * <pre>
     * Color represents a vertex color stream.
     * </pre>
     */
    public static final int Color_VALUE = 2;
    /**
     * <code>Texcoord = 3;</code>
     *
     * <pre>
     * Texcoord represents a texture coordinate stream.
     * </pre>
     */
    public static final int Texcoord_VALUE = 3;
    /**
     * <code>Normal = 4;</code>
     *
     * <pre>
     * Normal represents a vertex normal stream.
     * </pre>
     */
    public static final int Normal_VALUE = 4;
    /**
     * <code>Tangent = 5;</code>
     *
     * <pre>
     * Tangent represents a vertex tangent stream.
     * </pre>
     */
    public static final int Tangent_VALUE = 5;
    /**
     * <code>Bitangent = 6;</code>
     *
     * <pre>
     * Bitangent represents a vertex bitangent stream.
     * </pre>
     */
    public static final int Bitangent_VALUE = 6;


    public final int getNumber() {
      if (index == -1) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    public static SemanticType valueOf(int value) {
      switch (value) {
        case 0: return Unknown;
        case 1: return Position;
        case 2: return Color;
        case 3: return Texcoord;
        case 4: return Normal;
        case 5: return Tangent;
        case 6: return Bitangent;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<SemanticType>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        SemanticType> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<SemanticType>() {
            public SemanticType findValueByNumber(int number) {
              return SemanticType.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return com.android.tools.idea.editors.gfxtrace.service.vertex.VertexProtos.getDescriptor().getEnumTypes().get(0);
    }

    private static final SemanticType[] VALUES = values();

    public static SemanticType valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private SemanticType(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:vertex.SemanticType)
  }

  /**
   * Protobuf enum {@code vertex.VectorElement}
   *
   * <pre>
   * VectorElement represents a single vector element
   * </pre>
   */
  public enum VectorElement
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>X = 0;</code>
     *
     * <pre>
     * X represents the x-axis of a vector.
     * </pre>
     */
    X(0, 0),
    /**
     * <code>Y = 1;</code>
     *
     * <pre>
     * Y represents the y-axis of a vector.
     * </pre>
     */
    Y(1, 1),
    /**
     * <code>Z = 2;</code>
     *
     * <pre>
     * Z represents the z-axis of a vector.
     * </pre>
     */
    Z(2, 2),
    /**
     * <code>W = 3;</code>
     *
     * <pre>
     * W represents the w-axis of a vector.
     * </pre>
     */
    W(3, 3),
    UNRECOGNIZED(-1, -1),
    ;

    /**
     * <code>X = 0;</code>
     *
     * <pre>
     * X represents the x-axis of a vector.
     * </pre>
     */
    public static final int X_VALUE = 0;
    /**
     * <code>Y = 1;</code>
     *
     * <pre>
     * Y represents the y-axis of a vector.
     * </pre>
     */
    public static final int Y_VALUE = 1;
    /**
     * <code>Z = 2;</code>
     *
     * <pre>
     * Z represents the z-axis of a vector.
     * </pre>
     */
    public static final int Z_VALUE = 2;
    /**
     * <code>W = 3;</code>
     *
     * <pre>
     * W represents the w-axis of a vector.
     * </pre>
     */
    public static final int W_VALUE = 3;


    public final int getNumber() {
      if (index == -1) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    public static VectorElement valueOf(int value) {
      switch (value) {
        case 0: return X;
        case 1: return Y;
        case 2: return Z;
        case 3: return W;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<VectorElement>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        VectorElement> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<VectorElement>() {
            public VectorElement findValueByNumber(int number) {
              return VectorElement.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return com.android.tools.idea.editors.gfxtrace.service.vertex.VertexProtos.getDescriptor().getEnumTypes().get(1);
    }

    private static final VectorElement[] VALUES = values();

    public static VectorElement valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private VectorElement(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:vertex.VectorElement)
  }


  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\031gapid/vertex/vertex.proto\022\006vertex*j\n\014S" +
      "emanticType\022\013\n\007Unknown\020\000\022\014\n\010Position\020\001\022\t" +
      "\n\005Color\020\002\022\014\n\010Texcoord\020\003\022\n\n\006Normal\020\004\022\013\n\007T" +
      "angent\020\005\022\r\n\tBitangent\020\006*+\n\rVectorElement" +
      "\022\005\n\001X\020\000\022\005\n\001Y\020\001\022\005\n\001Z\020\002\022\005\n\001W\020\003BF\n6com.andr" +
      "oid.tools.idea.editors.gfxtrace.service." +
      "vertexB\014VertexProtosb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
