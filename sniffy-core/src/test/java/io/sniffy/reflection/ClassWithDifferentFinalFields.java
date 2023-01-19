package io.sniffy.reflection;

class ClassWithDifferentFinalFields {

    private final Object privateObjectField;
    private final int privateIntField;
    private final boolean privateBooleanField;

    public ClassWithDifferentFinalFields(Object privateObjectField, int privateIntField, boolean privateBooleanField) {
        this.privateObjectField = privateObjectField;
        this.privateIntField = privateIntField;
        this.privateBooleanField = privateBooleanField;
    }

}
