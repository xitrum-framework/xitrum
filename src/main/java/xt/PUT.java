package xt;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PUT {
  String value();
  boolean first() default false;
  boolean last()  default false;
}
