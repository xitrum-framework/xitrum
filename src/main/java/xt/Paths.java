package xt;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Java does not allow multiple annotations of the same type on one element:
 * http://stackoverflow.com/questions/1554112/multiple-annotations-of-the-same-type-on-one-element
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Paths {
  String[] value();
  boolean first() default false;
  boolean last()  default false;
}
