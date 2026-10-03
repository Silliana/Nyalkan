package net.nyalkan.interfaces;

public interface ExtendedModelPart {
    int nyalkan$stateSize();

    int nyalkan$writeState(float[] dst, int offset);

    int nyalkan$readState(float[] src, int offset);

    boolean nyalkan$hasGeometry();
}
