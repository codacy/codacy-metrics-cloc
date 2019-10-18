# #Metrics: {"loc": 18, "cloc": 1}
def on(definition, visit = false, &block)
  if @page.is_a?(definition)
    block.call @page if block
    return @page
  end

  if @context.is_a?(definition)
    block.call @context if block
    @page = @context unless @page.is_a?(definition)
    return @context
  end

  @page = definition.new(@browser)
  @page.view if visit

  @page.correct_url? if @page.respond_to?(:url_matches)
  @page.correct_title? if @page.respond_to?(:title_is)

  @model = @page

  block.call @page if block

  @page
end
