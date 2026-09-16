package cr.ac.ucr.paraiso.ie.c4h845.expresofast.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Sin @EnableJpaAuditing el listener nunca puebla @CreatedDate ni @LastModifiedDate. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
