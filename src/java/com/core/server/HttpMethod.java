package com.core.server;

import java.lang.annotation.*;

public class HttpMethod {
    @Documented
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HttpGet {
    }

    @Documented
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HttpPost {
    }
}
