package br.edu.ifrn.promptuario.iam;

import br.edu.ifrn.promptuario.iam.config.JwtProperties;
import br.edu.ifrn.promptuario.iam.config.OAuthConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, OAuthConfig.class})
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
