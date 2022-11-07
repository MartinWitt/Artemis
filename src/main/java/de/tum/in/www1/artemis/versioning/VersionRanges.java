package de.tum.in.www1.artemis.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Depicts a set of different ranges.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionRanges {

    VersionRange[] value() default {};
}
