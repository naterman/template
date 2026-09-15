from yaml import safe_load_all
import sys
from pathlib import Path


def main(path: Path):

    with open(path, "r") as file:
        # 2. Use safe_load_all because the YAML contains multiple documents separated by '---'
        documents = safe_load_all(file)

        recipes = []
        # 3. Iterate through each parsed document
        for i, doc in enumerate(documents, start=1):
            # The first document might be empty because of the leading '---' after comments
            if doc is None:
                continue

            print(f"--- Document {i} ---")
            print(f"Name: {doc.get('name')}")
            print(f"Display Name: {doc.get('displayName')}")

            # Example of accessing nested list elements
            recipe_list = doc.get("recipeList", [])
            print(f"Number of recipes included: {len(recipe_list)}\n")
            for recipe in recipe_list:
                if isinstance(recipe, dict):
                    recipes.append(list(recipe.keys())[0])
                else:
                    recipes.append(recipe)

            print(len(recipes))
            print(len(set(recipes)))

            seen = set()
            duplicates = set()

            for item in recipes:
                if item in seen:
                    duplicates.add(item)
                else:
                    seen.add(item)

            for item in set(duplicates):
                print(item)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise Exception("Must Provide File")
    main(Path(sys.argv[-1]).resolve())
