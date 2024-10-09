defmodule Scanner do
  def new(iodev) do
    stream = PeekableStream.new(IO.stream(iodev, 1))
    spawn_link(fn -> loop(stream, Counter.start(1)) end)
  end

  def next_token(pid), do: send_msg(pid, :next)
  def stop(pid), do: send(pid, :stop)

  defp loop(stream, line) do
    receive do
      {:next, caller} ->
        send(caller, {:next, self(), scan(stream, line)})
        loop(stream, line)
    end
  end

  defp scan(stream, line, lexeme \\ "") do
    char = PeekableStream.next(stream)

    mkToken = fn type, char ->
      %Token{type: type, lexeme: char, line: Counter.value(line)}
    end

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

      "!" ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:bang_equal, char <> peek)

          _ ->
            mkToken.(:bang, char)
        end

      "=" ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:equal_equal, char <> peek)

          _ ->
            mkToken.(:equal, char)
        end

      "<" ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:less_equal, char <> peek)

          _ ->
            mkToken.(:less, char)
        end

      ">" ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:greater_equal, char <> peek)

          _ ->
            mkToken.(:greater, char)
        end

      char when char in ["\s", "\r", "\t"] ->
        scan(stream, line, lexeme)

      "/" ->
        case PeekableStream.peek(stream) do
          "/" ->
            consume_line(stream)
            scan(stream, line, lexeme)

          _ ->
            mkToken.(:slash, char)
        end

      "\n" ->
        Counter.increment(line)
        scan(stream, line, lexeme)

      :eof ->
        :eof
    end
  end

  # TODO dedupe all of these. Maybe this is what agents and genservers are for.
  defp send_msg(pid, kind) do
    send(pid, {kind, self()})

    receive do
      {^kind, ^pid, char} -> char
    end
  end

  defp consume_line(stream) do
    case PeekableStream.peek(stream) do
      char when char in [:eof, "\n"] ->
        nil

      _ ->
        PeekableStream.next(stream)
        consume_line(stream)
    end
  end
end
