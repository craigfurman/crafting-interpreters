defmodule Scanner do
  # TODO return a lazy stream to avoid buffering all tokens in memory
  def scan(stream) do
    line_counter = Counter.start(1)

    mkToken = fn type, char ->
      %Token{type: type, lexeme: char, line: Counter.value(line_counter)}
    end

    tokens =
      for char <- stream do
        case char do
          "(" ->
            mkToken.(:left_paren, char)

          ")" ->
            mkToken.(:right_paren, char)

          "{" ->
            mkToken.(:left_brace, char)

          "}" ->
            mkToken.(:right_brace, char)

          "[" ->
            mkToken.(:left_bracket, char)

          "]" ->
            mkToken.(:right_bracket, char)

          "," ->
            mkToken.(:comma, char)

          "." ->
            mkToken.(:dot, char)

          "-" ->
            mkToken.(:minus, char)

          "+" ->
            mkToken.(:plus, char)

          ";" ->
            mkToken.(:semicolon, char)

          "*" ->
            mkToken.(:star, char)

          "\n" ->
            Counter.increment(line_counter)
            nil
        end
      end
      |> Enum.filter(&(&1 != nil))

    Counter.stop(line_counter)
    tokens
  end
end
