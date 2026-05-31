{
  description = "sourceline-manager — composable ADT for source-code generation";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
    in {
      devShells.${system}.default = pkgs.mkShell {
        name = "sourceline-manager-dev";
        nativeBuildInputs = with pkgs; [
          mill
          scala-cli
          openjdk
          # Scala Native toolchain
          clang
          llvmPackages.libcxxClang
          boehmgc
          zlib
          which
        ];
      };
    };
}
