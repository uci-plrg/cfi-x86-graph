package edu.uci.plrg.cfi.x86.graph.data.graph.modular;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uci.plrg.cfi.common.exception.InvalidGraphException;
import edu.uci.plrg.cfi.x86.graph.data.application.ApplicationModule;
import edu.uci.plrg.cfi.x86.graph.data.application.ApplicationModuleSet;
import edu.uci.plrg.cfi.x86.graph.data.graph.MetaNodeType;
import edu.uci.plrg.cfi.x86.graph.data.graph.Node;
import edu.uci.plrg.cfi.x86.graph.util.CrowdSafeTraceUtil;

public class ModuleBoundaryNode extends ModuleNode<ModuleBoundaryNode.Key> {

	public static class Key implements Node.Key {
		private final long hash;

		private final MetaNodeType type;

		public Key(long hash, MetaNodeType type) {
			if (hash == 0L)
				throw new IllegalArgumentException("ClusterBoundaryNode hash cannot be zero!");

			this.hash = hash;
			this.type = type;
		}

		@Override
		public boolean isModuleRelativeEquivalent(edu.uci.plrg.cfi.x86.graph.data.graph.Node.Key other) {
			if (other instanceof Key) {
				Key otherKey = (Key) other;
				return (type == otherKey.type) && (hash == otherKey.hash);
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (hash ^ (hash >>> 32));
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (hash != other.hash)
				return false;
			if (type != other.type)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return String.format("%s(0x%x)", type, hash);
		}
	}

	public enum HashLabelProperty {
		HASH_LABEL_FROM_ANONYMOUS,
		HASH_LABEL_TO_ANONYMOUS,
		HASH_LABEL_CALLBACK,
		HASH_LABEL_INTERCEPTION,
		HASH_LABEL_GENCODE,
		HASH_LABEL_EXPORT;
	}

	public static class HashLabel {

		public static HashLabel createAnonymousEntry(String fromFilename) {
			String label = String.format("%s/<anonymous>!callback", fromFilename);
			long hash = CrowdSafeTraceUtil.stringHash(label);

			HashLabel hashLabel = new HashLabel(label, hash, 0, fromFilename, ApplicationModule.ANONYMOUS_MODULE_ID);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_TO_ANONYMOUS);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_CALLBACK);
			return hashLabel;
		}

		public static HashLabel createAnonymousExit(String toFilename) {
			String label = String.format("<anonymous>/%s!callback", toFilename);
			long hash = CrowdSafeTraceUtil.stringHash(label);

			HashLabel hashLabel = new HashLabel(label, hash, 0, ApplicationModule.ANONYMOUS_MODULE_ID, toFilename);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_FROM_ANONYMOUS);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_CALLBACK);
			return hashLabel;
		}

		public static HashLabel createGencodeEntry(String fromFilename) {
			String label = String.format("%s/<anonymous>!gencode", fromFilename);
			long hash = CrowdSafeTraceUtil.stringHash(label);

			HashLabel hashLabel = new HashLabel(label, hash, 0, fromFilename, ApplicationModule.ANONYMOUS_MODULE_ID);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_TO_ANONYMOUS);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_GENCODE);
			return hashLabel;
		}

		public static HashLabel createInterception(String fromFilename) {
			String label = String.format("%s!interception", fromFilename);
			long hash = CrowdSafeTraceUtil.stringHash(label);

			HashLabel hashLabel = new HashLabel(label, hash, 0, null, null);
			hashLabel.properties.add(HashLabelProperty.HASH_LABEL_INTERCEPTION);
			return hashLabel;
		}

		private static final Pattern ENTRY_PATTERN = Pattern.compile("^0x([0-9a-f]+) ([^0].*) 0x([0-9a-f]+)$");

		private static final String CALLBACK_TAG = "!callback";
		private static final String GENCODE_TAG = "->gencode";
		private static final String DGC_TAG = "->dgc";
		private static final String ANONYMOUS_TAG = "anonymous";
		private static final String FROM_ANONYMOUS_TAG = "anonymous/";
		private static final String TO_ANONYMOUS_TAG = "/anonymous";

		public final String label;
		public final long hash;
		public final int offset;
		public final String fromModuleFilename;
		public final String toModuleFilename;

		private final Set<HashLabelProperty> properties = EnumSet.noneOf(HashLabelProperty.class);

		private HashLabel(String label, long hash, int offset, String fromModuleFilename, String toModuleFilename) {
			this.label = label;
			this.hash = hash;
			this.offset = offset;
			this.fromModuleFilename = fromModuleFilename;
			this.toModuleFilename = toModuleFilename;
		}

		public HashLabel(String xhashEntry) {
			Matcher matcher = ENTRY_PATTERN.matcher(xhashEntry);

			if (!matcher.matches())
				throw new InvalidGraphException("Failed to parse xhash entry %s", xhashEntry);

			hash = new BigInteger(matcher.group(1), 0x10).longValue();
			label = matcher.group(2);
			offset = (int) Long.parseLong(matcher.group(3), 0x10);

			if (label.endsWith(DGC_TAG) || label.endsWith(GENCODE_TAG)) {
				fromModuleFilename = label.substring(0, label.indexOf('@'));
				toModuleFilename = null;
			} else {
				int slashIndex = label.indexOf('/');
				if (slashIndex < 0) {
					fromModuleFilename = toModuleFilename = null;
				} else {
					fromModuleFilename = label.substring(0, slashIndex);
					toModuleFilename = label.substring(slashIndex + 1, label.indexOf('!'));
				}
			}

			if (label.startsWith(FROM_ANONYMOUS_TAG)
					|| (fromModuleFilename != null && fromModuleFilename.equals(ANONYMOUS_TAG)))
				properties.add(HashLabelProperty.HASH_LABEL_FROM_ANONYMOUS);
			else if (label.contains(TO_ANONYMOUS_TAG) || label.endsWith(DGC_TAG) || label.endsWith(GENCODE_TAG))
				properties.add(HashLabelProperty.HASH_LABEL_TO_ANONYMOUS);
			else if (!label.contains("/"))
				properties.add(HashLabelProperty.HASH_LABEL_EXPORT);

			if (label.endsWith(CALLBACK_TAG))
				properties.add(HashLabelProperty.HASH_LABEL_CALLBACK);
			else if (label.endsWith(GENCODE_TAG))
				properties.add(HashLabelProperty.HASH_LABEL_GENCODE);
		}

		public boolean isFromAnonymous() {
			return properties.contains(HashLabelProperty.HASH_LABEL_FROM_ANONYMOUS);
		}

		public boolean isToAnonymous() {
			return properties.contains(HashLabelProperty.HASH_LABEL_TO_ANONYMOUS);
		}

		public boolean isGencode() {
			return properties.contains(HashLabelProperty.HASH_LABEL_GENCODE);
		}

		public boolean isCallback() {
			return properties.contains(HashLabelProperty.HASH_LABEL_CALLBACK);
		}

		public boolean isInterception() {
			return properties.contains(HashLabelProperty.HASH_LABEL_INTERCEPTION);
		}

		public boolean isExport() {
			return properties.contains(HashLabelProperty.HASH_LABEL_EXPORT);
		}

		@Override
		public String toString() {
			return String.format("0x%x:%s", hash, label);
		}
	}

	public final HashLabel hashLabel;

	public ModuleBoundaryNode(long hash, MetaNodeType type) {
		super(new Key(hash, type));

		hashLabel = ApplicationModuleSet.getInstance().crossModuleLabels.get(hash);

		if ((type != MetaNodeType.MODULE_ENTRY) && (type != MetaNodeType.MODULE_EXIT))
			throw new IllegalArgumentException(String.format(
					"Cluster boundary node must have type %s or %s. Given type is %s.", MetaNodeType.MODULE_ENTRY,
					MetaNodeType.MODULE_EXIT, type));
	}

	@Override
	public ApplicationModule getModule() {
		return ApplicationModule.BOUNDARY_MODULE;
	}

	@Override
	public int getRelativeTag() {
		return 0;
	}

	@Override
	public int getInstanceId() {
		return 0;
	}

	@Override
	public long getHash() {
		return key.hash;
	}

	@Override
	public MetaNodeType getType() {
		return key.type;
	}

	private String limit(String src, int limit) {
		return src.length() > limit ? src.substring(0, limit) : src;
	}

	public String identify() {
		if (hashLabel == null)
			return String.format("(0x%x|%s)", key.hash, key.type.code);

		String fileContext = (key.type == MetaNodeType.MODULE_ENTRY ? hashLabel.fromModuleFilename
				: hashLabel.toModuleFilename);

		String target = "";
		if (hashLabel.isCallback()) {
			target = "|callback";
		} else if (hashLabel.isInterception()) {
			target = "|interception";
		} else if (hashLabel.isExport() || hashLabel.isToAnonymous()) {
			target = limit(hashLabel.label, 30);
			fileContext = "";
		}

		return String.format("(%s%s|%s)", fileContext, target, key.type.code);
	}

	@Override
	public String toString() {
		return identify();
	}
}
