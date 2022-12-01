job "copier-coller-api" {
  datacenters = ["${datacenter}"]
  type = "service"
  namespace = "${nomad_namespace}"

  vault {
    policies = ["poc-policy"]
    change_mode = "restart"
  }

  affinity {
    attribute = "$\u007Bnode.class\u007D"
    value = "standard"
  }

  group "copier-coller-api" {
    count = "1"
    restart {
      attempts = 3
      delay = "60s"
      interval = "1h"
      mode = "fail"
    }

    network {
      port "http" {
        to = 8080
      }
    }

    task "api" {
      driver = "docker"

      artifact {
        source = "https://github.com/prosanteconnect/copier-coller/raw/main/resources/patient-info.json"
        options {
          archive = "false"
        }
      }
      
      artifact {
        source = "https://github.com/prosanteconnect/copier-coller/raw/main/resources/alternative-schema.json"
        options {
          archive = "false"
        }
      }

      config {
        image = "${artifact.image}:${artifact.tag}"
        ports = ["http"]
        mount {
          type = "bind"
          target = "/app/json-schemas-repo/patient-info.json"
          source = "local/patient-info.json"
          readonly = "false"
          bind_options {
            propagation = "rshared"
          }
        }
        mount {
          type = "bind"
          target = "/app/json-schemas-repo/alternative-schema.json"
          source = "local/alternative-schema.json"
          readonly = "false"
          bind_options {
            propagation = "rshared"
          }
        }
      }

      # env variables
      template {
        destination = "local/file.env"
        env = true
        data = <<EOH
PUBLIC_HOSTNAME={{ with secret "poc/copier-coller" }}{{ .Data.data.api_public_hostname }}{{ end }}
JAVA_TOOL_OPTIONS="-Xms256m -Xmx1g -XX:+UseG1GC -Dspring.config.location=/secrets/application.properties -Dlogging.level.fr.ans.psc=${log_level}"
EOH
      }

      #application.properties
      template {
        destination = "secrets/application.properties"
        change_mode = "restart"
        data = <<EOF
spring.application.name=psc-copier-coller-api
server.servlet.contextPath=/psc-copier-coller/api
logging.level.org.springframework=ERROR

#ProSanteConnect introspection Endpoint
{{ with secret "poc/copier-coller" }}
psc.url.introspection={{ .Data.data.introspection_url }}
psc.url.userinfo={{ .Data.data.userinfo_url }}
psc.clientID={{ .Data.data.psc_client_id }}
psc.clientSecret={{ .Data.data.psc_client_secret }}
{{ end }}

spring.redis.database=0
spring.redis.host={{ range service "${nomad_namespace}-redis-copier-coller" }}{{ .Address }}{{ end }}
spring.redis.port={{ range service "${nomad_namespace}-redis-copier-coller" }}{{ .Port }}{{ end }}
{{ with secret "poc/copier-coller" }}
#spring.redis.username={{ .Data.data.redis_username }}
spring.redis.password={{ .Data.data.redis_password }}
{{ end }}
schemas.file.repository=/app/json-schemas-repo
EOF
      }

      resources {
        cpu = 500
        memory = 1152
      }

      service {
        name = "$\u007BNOMAD_NAMESPACE\u007D-$\u007BNOMAD_JOB_NAME\u007D"
        tags = ["urlprefix-/psc-copier-coller/"]
        port = "http"
        check {
          type = "http"
          path = "/psc-copier-coller/api/check"
          port = "http"
          interval = "30s"
          timeout = "2s"
          failures_before_critical = "3"
        }
      }
    }
  }
}
