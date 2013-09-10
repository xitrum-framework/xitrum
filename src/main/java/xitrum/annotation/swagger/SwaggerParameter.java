package xitrum.annotation.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerParameter {
  String  name()          default "";
  String  typename()      default "";
  String  description()   default "";
  boolean required()      default false;
  boolean allowMultiple() default false;
}
