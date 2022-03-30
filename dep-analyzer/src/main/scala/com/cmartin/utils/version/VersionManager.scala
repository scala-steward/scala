package com.cmartin.utils.version

import com.cmartin.utils.model.Domain.{ComparatorResult, Gav}
import zio.{Accessible, UIO}

trait VersionManager {
  def compare(local: Gav, remote: Gav): UIO[ComparatorResult]
}

object VersionManager extends Accessible[VersionManager]
