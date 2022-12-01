job "redis-copier-coller" {
  datacenters = [
    "${datacenter}"
  ]
  type = "service"
  namespace = "${nomad_namespace}"

  vault {
    policies = [
      "poc-policy"]
    change_mode = "restart"
  }

  group "redis-copier-coller" {
    count = "1"

    restart {
      attempts = 3
      delay = "60s"
      interval = "1h"
      mode = "fail"
    }

    constraint {
      attribute = "$\u007Bnode.class\u007D"
      value = "data"
    }

    network {
      port "db" {
        to = 6379
      }
    }

    task "redis" {
      driver = "docker"

      template {
        destination = "secrets/redis.conf"
        change_mode = "restart"
        data = <<EOH
{{ with secret "psc-ecosystem/${nomad_namespace}" }}
#user {{ .Data.data.redis_username }}
requirepass {{ .Data.data.redis_password }}
{{ end }}
EOH
      }

      config {
        image = "${image}:${tag}"
        ports = [
          "db"]

        mount {
          type = "bind"
          target = "/usr/local/etc/redis/redis.conf"
          source = "secrets/redis.conf"
          readonly = false
          bind_options {
            propagation = "rshared"
          }
        }
      }

      resources {
        cpu = 500
        memory = 1280
      }

      service {
        name = "$\u007BNOMAD_NAMESPACE\u007D-$\u007BNOMAD_JOB_NAME\u007D"
        port = "db"
        check {
          name = "alive"
          type = "tcp"
          interval = "30s"
          timeout = "5s"
          failures_before_critical = 3
          port = "db"
        }
      }
    }
  }
}
