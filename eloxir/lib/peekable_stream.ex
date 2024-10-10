defmodule PeekableStream do
  def new(stream), do: %{stream: stream, buffer: next_elem(stream)}

  def next(state), do: {state.buffer, %{state | buffer: next_elem(state.stream)}}
  def peek(state), do: {state.buffer, state}

  defp next_elem(stream) do
    case Stream.take(stream, 1) |> Enum.to_list() do
      [] -> :eof
      [char] -> char
    end
  end
end
