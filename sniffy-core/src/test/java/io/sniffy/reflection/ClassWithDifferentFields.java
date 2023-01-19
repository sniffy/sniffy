package io.sniffy.reflection;

class ClassWithDifferentFields {

    private Object privateObjectField;
    private int privateIntField;
    private boolean privateBooleanField;

    public ClassWithDifferentFields(Object privateObjectField, int privateIntField, boolean privateBooleanField) {
        this.privateObjectField = privateObjectField;
        this.privateIntField = privateIntField;
        this.privateBooleanField = privateBooleanField;
    }

}
