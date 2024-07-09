package de.cronn.postgres.snapshot.util;

import java.util.ArrayList;
import java.util.List;

public record Schema(String pattern, Type type) {

	public static Schema include(String pattern) {
		return new Schema(pattern, Type.INCLUDE);
	}

	public static Schema exclude(String pattern) {
		return new Schema(pattern, Type.EXCLUDE);
	}

	List<String> getCommandArguments() {
		List<String> args = new ArrayList<>();
		String parameter = switch (type()) {
			case INCLUDE -> "-n";
			case EXCLUDE -> "-N";
		};
		args.add(parameter);
		args.add(pattern());
		return args;
	}

	public enum Type {
		INCLUDE,
		EXCLUDE,
	}
}
