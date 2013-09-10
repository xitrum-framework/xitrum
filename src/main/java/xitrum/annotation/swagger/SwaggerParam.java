package xitrum.annotation.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/** https://github.com/wordnik/swagger-core/wiki/parameters */
public @interface SwaggerParam {
  // Default param type is "path", which is required
  String  name();
  String  paramType()   default "path";
  String  valueType();
  boolean required()    default true;
  String  description() default "";
}
