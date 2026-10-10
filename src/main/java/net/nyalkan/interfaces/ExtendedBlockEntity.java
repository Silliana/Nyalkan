package net.nyalkan.interfaces;

public interface ExtendedBlockEntity extends ReplayHolder {

    Object nyalkan$renderer(int epoch);

    void nyalkan$setRenderer(Object renderer, int epoch);

    Object nyalkan$bakeKey();

    void nyalkan$setBakeKey(Object key);
}
