package misk.crypto

/**
 * [LocalConfigKeyResolver] provides keys that are stored locally and protected by a single KMS
 * key.
 */
class LocalConfigKeyResolver(
  private val keys: List<Key>,
  private val defaultKmsUri: String
) : KeyResolver {

  override val allKeyAliases: Map<KeyAlias, KeyType> =
    keys.map { key -> key.key_name to key.key_type }.toMap()

  override fun getKeyByAlias(alias: KeyAlias): Key? {
    return keys.find { key -> key.key_name == alias }?.let { key ->
      if (key.key_type != KeyType.HYBRID_ENCRYPT) {
        key.copy(kms_uri = key.kms_uri ?: defaultKmsUri)
      } else {
        key
      }
    }
  }
}
