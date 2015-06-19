(ns reverie.nsloader
  (:require reverie.core
            reverie.settings

            ;; define protocols
            reverie.database
            reverie.migrator
            reverie.page
            reverie.object
            reverie.module
            reverie.module.entity
            reverie.admin.storage

            ;; implement protocols
            reverie.database.sql
            reverie.admin.storage.sql
            reverie.migrator.sql
            reverie.auth.sql
            reverie.cache.sql
            reverie.modules.sql

            ;; load admin
            reverie.admin.index

            ;load modules
            reverie.modules.auth
            reverie.modules.filemanager
            reverie.modules.role))
