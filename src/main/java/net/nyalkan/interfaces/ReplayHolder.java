package net.nyalkan.interfaces;

import net.nyalkan.render.model.OutputReplay;

public interface ReplayHolder {

    OutputReplay.Entry nyalkan$replay();

    void nyalkan$setReplay(OutputReplay.Entry entry);
}
