package xitrum.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerDoc {
  /** brief description of the operation  */
  String summary() default "";

  /** long description of the operation */
  String notes() default "";

  /** parameters */
  SwaggerParameter[] parameters() default {};
  
  /** errors */
  SwaggerErrorResponse[] errorResponses() default {};
  
}
