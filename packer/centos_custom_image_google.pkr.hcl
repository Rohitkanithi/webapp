packer {
  required_plugins {
    googlecompute = {
      source  = "github.com/hashicorp/googlecompute"
      version = "~> 1"
    }
  }
}

source "googlecompute" "centos8_image" {
  project_id            = var.project_id
  source_image_family   = var.source_image_family
  zone                  = var.custom_image_zone
  ssh_username          = var.ssh_username
  network               = var.custom_image_network
  image_name            = "${var.custom_image_name}-{{timestamp}}"
  image_description     = var.custom_image_description
  service_account_email = var.service_account_email
}

build {
  sources = ["source.googlecompute.centos8_image"]

  provisioner "shell" {
    script = "script/installation.sh"
  }
  provisioner "shell" {
    script = "script/user.sh"
  }
  provisioner "file" {
    source      = "./start-webapp.service"
    destination = "/tmp/"
  }
  provisioner "shell" {
    script = "./script/sysd_startup.sh"
  }
  provisioner "file" {
    source      = "../target/webapp-1.0-SNAPSHOT.jar"
    destination = "/tmp/webapp-1.0-SNAPSHOT.jar"
  }
  provisioner "shell" {
    script = "script/chown.sh"
  }
  provisioner "shell" {
    script = "script/install_ops.sh"
  }
  post-processor "manifest" {
    output     = "manifest.json"
    strip_path = true
  }
}