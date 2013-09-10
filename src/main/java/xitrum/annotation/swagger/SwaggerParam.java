package xitrum.annotation.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/** https://github.com/wordnik/swagger-core/wiki/parameters */
public @interface SwaggerParam {
  String  name();
  String  paramType()   default "path";
  String  tpe();
  String  description() default "";
  boolean required()    default true;
}
