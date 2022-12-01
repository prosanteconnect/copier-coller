project = "poc/copier-coller"

runner {
  enabled = true
  profile = "poc"
  data_source "git" {
    url = "https://github.com/prosanteconnect/copier-coller.git"
    ref = "deployment"
  }
  poll {
    enabled = false
  }
}

app "redis" {
  build {
    use "docker-pull" {
      image = "redis"
      tag = "latest"
    }
    registry {
      use "docker" {
        image = "prosanteconnect/redis"
        tag = "latest"
        username = var.registry_username
        password = var.registry_password
        local = true
      }
    }
  }

  deploy {
    use "nomad-jobspec" {
      jobspec = templatefile("${path.app}/redis-deployment/redis.nomad.tpl", {
        datacenter = var.datacenter
        image = "redis"
        tag = "latest"
        nomad_namespace = var.nomad_namespace
      })
    }
  }
}

#app "api" {
#
#}

#app "demo-app-1" {
#
#}

#app "demo-app-2" {
#
#}

variable "datacenter" {
  type = string
  default = ""
  env = ["NOMAD_DATACENTER"]
}

variable "nomad_namespace" {
  type = string
  default = ""
  env = ["NOMAD_NAMESPACE"]
}

variable "registry_username" {
  type    = string
  default = ""
  env     = ["REGISTRY_USERNAME"]
  sensitive = true
}

variable "registry_password" {
  type    = string
  default = ""
  env     = ["REGISTRY_PASSWORD"]
  sensitive = true
}

variable "log_level" {
  type = string
  default = "INFO"
}
