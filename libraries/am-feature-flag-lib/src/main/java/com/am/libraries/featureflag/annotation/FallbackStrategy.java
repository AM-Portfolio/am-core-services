package com.am.libraries.featureflag.annotation;

public enum FallbackStrategy {
    RETURN_NULL,
    RETURN_EMPTY_LIST,
    RETURN_FALSE,
    THROW_EXCEPTION
}
