import tomlkit
from tomlkit.items import Table
import sys
from pathlib import Path


def transform_lib_key(key: str) -> str:
    if "bom" in key:
        return f"implementation(platform(libs.{key.replace("-", ".")}))"
    else:
        return f"implementation(libs.{key.replace("-", ".")})"


def transform_plugin_key(key: str) -> str:
    return f"alias(libs.plugins.{key.replace("-", ".")})"


def main(path: Path) -> None:
    toml = None

    with open(path, "r", encoding="utf-8") as file:
        toml = tomlkit.load(file)

    libraries: Table = toml["libraries"]
    libraries_list = []
    for key, _ in libraries.items():
        libraries_list.append(transform_lib_key(key))

    plugins: Table = toml["plugins"]
    plugins_list = []
    for key, _ in plugins.items():
        plugins_list.append(transform_plugin_key(key))

    output = Path(__file__).resolve().parent.parent / "build" / "dependencyBlock"
    output.mkdir(parents=True, exist_ok=True)

    with open(output / "output.txt", "w", encoding="utf-8") as file:
        file.write("plugins {\n")
        for item in plugins_list:
            file.write(f"   {item}\n")
        file.write("}\n")

        file.write("dependencies {\n")
        for item in libraries_list:
            file.write(f"   {item}\n")
        file.write("}\n")


if __name__ == "__main__":
    path = Path(sys.argv[1])

    main(path)
