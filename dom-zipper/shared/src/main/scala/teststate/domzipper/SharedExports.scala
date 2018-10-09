package teststate.domzipper

trait SharedExports {

  final implicit def toMofNOps(i: Int): MofN.IntExt =
    new MofN.IntExt(i)

  final type HtmlScrub = teststate.domzipper.HtmlScrub
  final val  HtmlScrub = teststate.domzipper.HtmlScrub

  implicit def htmlScrub: HtmlScrub =
    HtmlScrub.default

  final val DomZipper = teststate.domzipper.DomZipper

  final type DomZipper[F[_], Dom, A, Self[G[_], B] <: DomZipper[G, Dom, B, Self]] =
    teststate.domzipper.DomZipper[F, Dom, A, Self]
}
