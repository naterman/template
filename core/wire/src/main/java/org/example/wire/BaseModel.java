package org.example.wire;

import org.example.api.IModel;
import org.example.wire.proto.PrimaryWire;

public class BaseModel implements IModel {

    @Override
    public String getId() {
        PrimaryWire wire = PrimaryWire.newBuilder().setQuery("value").build();

        return wire.getQuery();

    }
}
