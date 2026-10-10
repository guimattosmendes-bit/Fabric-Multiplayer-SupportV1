# Framed Fabric (Minecraft Java 26.3 / Fabric)

Blocos-moldura que imitam qualquer bloco solido, inspirados no mod FramedBlocks.

## Uso
- Coloque um bloco-moldura, depois **clique direito com um bloco solido** na mao para camufla-lo.
- **Martelo de Moldura** (clique direito): remove a camuflagem e devolve o bloco.
- Quebrar a moldura tambem solta o bloco camuflado.
- Receitas: Cubo = 8 gravetos em anel (rende 4). Laje, escada, painel, cerca e muro seguem as receitas
  vanilla, usando Cubos Moldura. Martelo: `[ II / SI / S  ]` com ferro e graveto.

## Blocos
Cubo, Laje, Escada, Painel (laje vertical), Cerca, Muro, Alcapao (abre/fecha com a mao vazia; 6 cubos rendem 2).

## Compilar
Requer **JDK 25**.

    ./gradlew build        # jar em build/libs/
    ./gradlew runClient    # abre o jogo em modo de desenvolvimento

Versoes: Minecraft 26.3, Fabric Loader 0.19.5, Loom 1.18-SNAPSHOT, Fabric API 0.162.0+26.3.

## Como funciona
O formato de cada bloco vem do `VoxelShape` do estado (lista de caixas). `FramedBlockStateModel` emite
o modelo do bloco camuflado uma vez por caixa e, via `QuadTransform`, recorta cada face do cubo para a
face da caixa, reinterpolando os UVs (textura alinhada ao mundo). Para adicionar um formato novo basta
criar um bloco com o `VoxelShape` desejado e um blockstate apontando para `framedfabric:framed`.

## Pendencias conhecidas
- Rampas/inclinados e portas ainda nao existem.
- Cores de bioma (grama, folhas) nao sao aplicadas ao bloco camuflado.
- Faces internas entre caixas (ex.: degraus da escada) sao emitidas mesmo escondidas.

## Gerar o .jar sem instalar nada (GitHub Actions)
1. Crie um repositorio no GitHub e envie todo o conteudo desta pasta (incluindo a pasta oculta `.github`).
2. Abra a aba **Actions**; o workflow "Build jar" roda sozinho (ou clique em *Run workflow*).
3. Ao terminar, abra a execucao e baixe o artefato **framedfabric-jar**. Use o `framedfabric-0.2.0.jar`
   (ignore o `-sources`). Coloque na pasta `mods` junto com o Fabric API 0.162.0+26.3.
