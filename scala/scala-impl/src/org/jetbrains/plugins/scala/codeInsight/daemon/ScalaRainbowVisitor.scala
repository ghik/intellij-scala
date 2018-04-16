package org.jetbrains.plugins.scala
package codeInsight
package daemon

import com.intellij.codeInsight.daemon.RainbowVisitor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.extensions.childOf
import org.jetbrains.plugins.scala.highlighter.DefaultHighlighter
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPatternList
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScClassParameter, ScParameter}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScValue, ScValueOrVariable, ScVariable}

class ScalaRainbowVisitor extends RainbowVisitor {

  override final def suitableForFile(file: PsiFile): Boolean = file match {
    case _: ScalaFile => true
    case _ => false
  }

  override final def visit(element: PsiElement): Unit = element match {
    case parameter: ScParameter =>
      addInfo(parameter, parameter.nameId)
    case element: ScValueOrVariable =>
      addInfo(element, element.declaredElements.map(_.nameId): _*)
    case reference@ScReferenceExpression(parameter: ScParameter) =>
      addReferenceInfo(parameter, reference)
    case reference@ScReferenceExpression((_: ScReferencePattern) childOf ((_: ScPatternList) childOf (element: ScValueOrVariable))) =>
      addReferenceInfo(element, reference)
    case _ =>
  }

  override final def clone(): ScalaRainbowVisitor = new ScalaRainbowVisitor

  private def addReferenceInfo(element: PsiElement, expression: ScReferenceExpression): Unit =
    addInfo(element, expression.nameId)

  private def addInfo(element: PsiElement, rainbowElements: PsiElement*): Unit = {
    import PsiTreeUtil.getContextOfType
    import ScalaRainbowVisitor.ColorKey

    for {
      ColorKey(colorKey) <- Some(element)
      context <- Option(getContextOfType(element, true, classOf[ScFunction]))
      rainbowElement <- rainbowElements
      info = getInfo(context, rainbowElement, rainbowElement.getText, colorKey)
    } addInfo(info)
  }
}

object ScalaRainbowVisitor {

  private object ColorKey {

    import DefaultHighlighter._

    def unapply(element: PsiElement): Option[TextAttributesKey] = element match {
      case _: ScClassParameter => None
      case _: ScParameter => Some(PARAMETER)
      case value: ScValue if value.isLocal => Some(LOCAL_VALUES)
      case variable: ScVariable if variable.isLocal => Some(LOCAL_VARIABLES)
      case _ => None
    }
  }
}